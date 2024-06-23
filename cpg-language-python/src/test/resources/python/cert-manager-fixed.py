# Copyright 2015 NEC Corporation.  All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License"); you may
# not use this file except in compliance with the License. You may obtain
# a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
# License for the specific language governing permissions and limitations
# under the License.

from oslo_log import log as logging
from oslo_utils import encodeutils

from magnum.common import cert_manager
from magnum.common import exception
from magnum.common import short_id
from magnum.common.x509 import operations as x509

import magnum.conf
import os
import shutil
import tempfile

CONDUCTOR_CLIENT_NAME = 'Magnum-Conductor'

LOG = logging.getLogger(__name__)
CONF = magnum.conf.CONF


def _generate_ca_cert(issuer_name, context=None):
    """Generate and store ca_cert

    :param issuer_name: CA subject name
    :returns: CA cert uuid and CA cert, CA private key password
    """
    ca_password = short_id.generate_id()
    ca_cert = x509.generate_ca_certificate(issuer_name,
                                           encryption_password=ca_password)
    ca_cert_ref = cert_manager.get_backend().CertManager.store_cert(
        certificate=ca_cert['certificate'],
        private_key=ca_cert['private_key'],
        private_key_passphrase=ca_password,
        name=issuer_name,
        context=context,
    )
    LOG.debug('CA cert is created: %s', ca_cert_ref)
    return ca_cert_ref, ca_cert, ca_password


def _generate_client_cert(issuer_name, ca_cert, ca_password, context=None):
    """Generate and store magnum_client_cert

    :param issuer_name: CA subject name
    :param ca_cert: CA certificate
    :param ca_password: CA private key password
    :returns: Magnum client cert uuid
    """
    client_password = short_id.generate_id()
    # TODO(strigazi): set subject name and organization per driver
    # For RBAC kubernetes cluster we need the client to have:
    # subject_name: admin
    # organization_name system:masters
    # Non kubernetes drivers are not using the certificates fields
    # for authorization
    subject_name = 'admin'
    organization_name = 'system:masters'
    client_cert = x509.generate_client_certificate(
        issuer_name,
        subject_name,
        organization_name,
        ca_cert['private_key'],
        encryption_password=client_password,
        ca_key_password=ca_password,
    )
    magnum_cert_ref = cert_manager.get_backend().CertManager.store_cert(
        certificate=client_cert['certificate'],
        private_key=client_cert['private_key'],
        private_key_passphrase=client_password,
        name=CONDUCTOR_CLIENT_NAME,
        context=context
    )
    LOG.debug('Magnum client cert is created: %s', magnum_cert_ref)
    return magnum_cert_ref


def _get_issuer_name(cluster):
    issuer_name = cluster.name
    # When user create a Cluster without name, the cluster.name is None.
    # We should use cluster.uuid as issuer name.
    if issuer_name is None:
        issuer_name = cluster.uuid
    return issuer_name


def generate_certificates_to_cluster(cluster, context=None):
    """Generate ca_cert and magnum client cert and set to cluster

    :param cluster: The cluster to set CA cert and magnum client cert
    :returns: CA cert uuid and magnum client cert uuid
    """
    try:
        issuer_name = _get_issuer_name(cluster)

        LOG.debug('Start to generate certificates: %s', issuer_name)

        ca_cert_ref, ca_cert, ca_password = _generate_ca_cert(issuer_name,
                                                              context=context)
        etcd_ca_cert_ref, _, _ = _generate_ca_cert(issuer_name,
                                                   context=context)
        fp_ca_cert_ref, _, _ = _generate_ca_cert(issuer_name,
                                                 context=context)
        magnum_cert_ref = _generate_client_cert(issuer_name,
                                                ca_cert,
                                                ca_password,
                                                context=context)

        cluster.ca_cert_ref = ca_cert_ref
        cluster.magnum_cert_ref = magnum_cert_ref
        cluster.etcd_ca_cert_ref = etcd_ca_cert_ref
        cluster.front_proxy_ca_cert_ref = fp_ca_cert_ref
    except Exception:
        LOG.exception('Failed to generate certificates for Cluster: %s',
                      cluster.uuid)
        raise exception.CertificatesToClusterFailed(cluster_uuid=cluster.uuid)


def get_cluster_ca_certificate(cluster, context=None, ca_cert_type=None):
    ref = cluster.ca_cert_ref
    if ca_cert_type == "etcd":
        ref = cluster.etcd_ca_cert_ref
    elif ca_cert_type in ["front_proxy", "front-proxy"]:
        ref = cluster.front_proxy_ca_cert_ref

    ca_cert = cert_manager.get_backend().CertManager.get_cert(
        ref,
        resource_ref=cluster.uuid,
        context=context
    )

    return ca_cert


def get_cluster_magnum_cert(cluster, context=None):
    magnum_cert = cert_manager.get_backend().CertManager.get_cert(
        cluster.magnum_cert_ref,
        resource_ref=cluster.uuid,
        context=context
    )

    return magnum_cert


def create_client_files(cluster, context=None):
    if not os.path.isdir(CONF.cluster.temp_cache_dir):
        LOG.debug("Certificates will not be cached in the filesystem: they "
                  "will be created as tempfiles.")
        ca_cert = get_cluster_ca_certificate(cluster, context)
        magnum_cert = get_cluster_magnum_cert(cluster, context)

        ca_file = tempfile.NamedTemporaryFile(mode="w+")
        ca_file.write(encodeutils.safe_decode(ca_cert.get_certificate()))
        ca_file.flush()

        key_file = tempfile.NamedTemporaryFile(mode="w+")
        key_file.write(encodeutils.safe_decode(
            magnum_cert.get_decrypted_private_key()))
        key_file.flush()

        cert_file = tempfile.NamedTemporaryFile(mode="w+")
        cert_file.write(encodeutils.safe_decode(magnum_cert.get_certificate()))
        cert_file.flush()

    else:
        cached_cert_dir = os.path.join(CONF.cluster.temp_cache_dir,
                                       cluster.uuid)
        cached_ca_file = os.path.join(cached_cert_dir, 'ca.crt')
        cached_key_file = os.path.join(cached_cert_dir, 'client.key')
        cached_cert_file = os.path.join(cached_cert_dir, 'client.crt')

        if not os.path.isdir(cached_cert_dir):
            os.mkdir(cached_cert_dir)

            ca_cert = get_cluster_ca_certificate(cluster, context)
            magnum_cert = get_cluster_magnum_cert(cluster, context)

            ca_file = open(cached_ca_file, "w+")
            os.chmod(cached_ca_file, 0o600)
            ca_file.write(encodeutils.safe_decode(ca_cert.get_certificate()))
            ca_file.flush()

            key_file = open(cached_key_file, "w+")
            os.chmod(cached_key_file, 0o600)
            key_file.write(encodeutils.safe_decode(
                magnum_cert.get_decrypted_private_key()))
            key_file.flush()

            cert_file = open(cached_cert_file, "w+")
            os.chmod(cached_cert_file, 0o600)
            cert_file.write(
                encodeutils.safe_decode(magnum_cert.get_certificate()))
            cert_file.flush()

        else:
            ca_file = open(cached_ca_file, "r")
            key_file = open(cached_key_file, "r")
            cert_file = open(cached_cert_file, "r")

    return ca_file, key_file, cert_file


def sign_node_certificate(cluster, csr, ca_cert_type=None, context=None):
    ref = cluster.ca_cert_ref
    if ca_cert_type == "etcd":
        ref = cluster.etcd_ca_cert_ref
    elif ca_cert_type in ["front_proxy", "front-proxy"]:
        ref = cluster.front_proxy_ca_cert_ref

    ca_cert = cert_manager.get_backend().CertManager.get_cert(
        ref,
        resource_ref=cluster.uuid,
        context=context
    )

    node_cert = x509.sign(csr,
                          _get_issuer_name(cluster),
                          ca_cert.get_private_key(),
                          ca_cert.get_private_key_passphrase())
    return node_cert


def delete_certificates_from_cluster(cluster, context=None):
    """Delete ca cert and magnum client cert from cluster

    :param cluster: The cluster which has certs
    """
    for cert_ref in ['ca_cert_ref', 'magnum_cert_ref']:
        try:
            cert_ref = getattr(cluster, cert_ref, None)
            if cert_ref:
                cert_manager.get_backend().CertManager.delete_cert(
                    cert_ref, resource_ref=cluster.uuid, context=context)
        except Exception:
            LOG.warning("Deleting certs is failed for Cluster %s",
                        cluster.uuid)


def delete_client_files(cluster, context=None):
    cached_cert_dir = os.path.join(CONF.cluster.temp_cache_dir,
                                   cluster.uuid)
    try:
        if os.path.isdir(cached_cert_dir):
            shutil.rmtree(cached_cert_dir)
    except Exception:
        LOG.warning("Deleting client files failed for Cluster %s",
                    cluster.uuid)