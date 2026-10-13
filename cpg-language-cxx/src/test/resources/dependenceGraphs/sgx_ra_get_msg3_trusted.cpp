// EXTRACTED ECALL

sgx_status_t sgx_sgx_ra_get_msg3_trusted(void *pms)

{
  long lVar1;
  undefined8 uVar2;
  int iVar3;
  undefined4 uVar4;
  void *__ptr;
  sgx_report_t *_tmp_qe_report;
  sgx_ra_msg3_t *_tmp_p_msg3;
  
  if (pms == (void *)0x0) {
    return SGX_ERROR_INVALID_PARAMETER;
  }
  iVar3 = sgx_is_outside_enclave(pms,0x28);
  if (iVar3 != 0) {
    lVar1 = *(long *)((long)pms + 0x10);
    uVar2 = *(undefined8 *)((long)pms + 0x18);
    if (lVar1 == 0) {
      uVar4 = sgx_ra_get_msg3_trusted
                        (*(undefined4 *)((long)pms + 4),*(undefined4 *)((long)pms + 8),0,uVar2,
                         *(undefined4 *)((long)pms + 0x20));
      *(undefined4 *)pms = uVar4;
      return SGX_SUCCESS;
    }
    iVar3 = sgx_is_outside_enclave(lVar1,0x1b0);
    if (iVar3 != 0) {
      __ptr = dlmalloc(0x1b0);
      if (__ptr == (void *)0x0) {
        return SGX_ERROR_OUT_OF_MEMORY;
      }
      iVar3 = memcpy_s(__ptr,0x1b0,lVar1,0x1b0);
      if (iVar3 == 0) {
        uVar4 = sgx_ra_get_msg3_trusted
                          (*(undefined4 *)((long)pms + 4),*(undefined4 *)((long)pms + 8),__ptr,uVar2
                           ,*(undefined4 *)((long)pms + 0x20));
        *(undefined4 *)pms = uVar4;
      }
      free(__ptr);
      return (uint)(iVar3 != 0);
    }
  }
  return SGX_ERROR_INVALID_PARAMETER;
}

ulong sgx_ra_get_msg3_trusted(uint param_1,uint param_2,long param_3,ulong param_4,ulong param_5)

{
  uint uVar1;
  int iVar2;
  ulong uVar3;
  ulong uVar4;
  long lVar5;
  ulong uVar6;
  undefined4 *puVar7;
  uint uVar8;
  long in_FS_OFFSET;
  byte bVar9;
  long local_210;
  long local_208;
  long local_200;
  undefined local_1f8 [16];
  undefined local_1e8 [64];
  undefined local_1a8 [256];
  undefined local_a8 [16];
  undefined local_98 [16];
  undefined4 local_88 [8];
  undefined local_68 [40];
  long local_40;
  
  bVar9 = 0;
  local_40 = *(long *)(in_FS_OFFSET + 0x28);
  uVar1 = vector_size(g_ra_db);
  if (((param_1 < uVar1) && (param_2 != 0 && param_3 != 0)) && (param_4 != 0)) {
    local_210 = 0;
    sgx_spin_lock(&g_ra_db_lock);
    iVar2 = vector_get(g_ra_db,param_1,&local_210);
    if ((iVar2 == 0) && (local_210 != 0)) {
      sgx_spin_unlock(&g_ra_db_lock);
      if ((((param_5 & 0xffffffff) <= ~param_4) && (param_2 < 0xfffffeb0)) &&
         (((param_5 & 0xffffffff) == (ulong)param_2 + 0x150 &&
          (iVar2 = sgx_is_outside_enclave(param_4), iVar2 != 0)))) {
        uVar3 = sgx_verify_report(param_3);
        if ((int)uVar3 != 0) {
          if ((int)uVar3 == 0x3001) goto LAB_0014d845;
LAB_0014d81b:
          if ((int)uVar3 == 3) goto LAB_0014d845;
LAB_0014d820:
          uVar3 = 1;
          goto LAB_0014d845;
        }
        sgx_spin_lock(local_210 + 0x434);
        if (*(int *)(local_210 + 0x430) != 2) {
          sgx_spin_unlock(local_210 + 0x434);
          uVar3 = 5;
          goto LAB_0014d845;
        }
        iVar2 = memcmp(param_3 + 0x30,local_210 + 0x250,0x10);
        if ((iVar2 != 0) || (iVar2 = memcmp(param_3 + 0x40,local_210 + 0x230,0x20), iVar2 != 0)) {
          sgx_spin_unlock(local_210 + 0x434);
          uVar3 = 2;
          goto LAB_0014d845;
        }
        memcpy(local_1e8,local_210,0x40);
        memcpy(local_1a8,local_210 + 0xf0,0x100);
        memcpy(local_a8,local_210 + 0x210,0x10);
        sgx_spin_unlock(local_210 + 0x434);
        local_208 = 0;
        local_200 = 0;
        puVar7 = local_88;
        for (lVar5 = 8; lVar5 != 0; lVar5 = lVar5 + -1) {
          *puVar7 = 0;
          puVar7 = puVar7 + (ulong)bVar9 * -2 + 1;
        }
        uVar3 = sgx_sha256_init(&local_208);
        if ((int)uVar3 != 0) goto LAB_0014d81b;
        if (local_208 == 0) goto LAB_0014d820;
        uVar1 = sgx_sha256_update(local_210 + 0x220,0x10);
        if ((uVar1 != 0) || (uVar1 = sgx_cmac128_init(local_a8,&local_200), uVar1 != 0))
        goto joined_r0x0014db39;
        if (local_200 == 0) {
LAB_0014d9de:
          uVar3 = 1;
        }
        else {
          uVar1 = sgx_cmac128_update(local_1e8,0x40);
          if ((uVar1 == 0) && (uVar1 = sgx_cmac128_update(local_1a8,0x100,local_200), uVar1 == 0)) {
            uVar3 = param_4 + 0x150;
            uVar8 = 0x20;
            uVar4 = uVar3 + param_2;
            for (uVar6 = uVar3; uVar6 < uVar4; uVar6 = uVar6 + 0x20) {
              if ((uint)((int)uVar4 - (int)uVar6) < uVar8) {
                uVar8 = ((int)uVar3 - (int)uVar6) + param_2;
              }
              memcpy(local_68,uVar6,uVar8);
              uVar1 = sgx_sha256_update(local_68,uVar8,local_208);
              if ((uVar1 != 0) || (uVar1 = sgx_cmac128_update(local_68,uVar8,local_200), uVar1 != 0)
                 ) goto joined_r0x0014db39;
            }
            uVar1 = sgx_sha256_get_hash(local_208,local_88);
            if (uVar1 != 0) goto joined_r0x0014db39;
            uVar1 = sgx_cmac128_final(local_200,local_98);
            uVar3 = (ulong)uVar1;
            if (uVar1 != 0) goto joined_r0x0014db39;
            iVar2 = memcmp(param_3 + 0x140,local_88,0x20);
            if (iVar2 == 0) {
              memcpy(local_1f8,local_98,0x10);
              memcpy(param_4,local_1f8,0x150);
            }
            else {
              uVar3 = 0x3001;
            }
          }
          else {
joined_r0x0014db39:
            if (uVar1 != 3) goto LAB_0014d9de;
            uVar3 = 3;
          }
        }
        memset_s(local_a8,0x10,0,0x10);
        sgx_sha256_close(local_208);
        if (local_200 != 0) {
          sgx_cmac128_close();
        }
        goto LAB_0014d845;
      }
    }
    else {
      sgx_spin_unlock(&g_ra_db_lock);
    }
  }
  uVar3 = 2;
LAB_0014d845:
  if (local_40 != *(long *)(in_FS_OFFSET + 0x28)) {
                    // WARNING: Subroutine does not return
    __stack_chk_fail();
  }
  return uVar3;
}

