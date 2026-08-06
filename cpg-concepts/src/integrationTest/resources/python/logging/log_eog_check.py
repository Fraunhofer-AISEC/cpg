import logging

"""
This code requires the `getLogger` call to be handled first before the `logger.info` call in order to have correct logger resolution. See https://github.com/Fraunhofer-AISEC/cpg/issues/2479
"""

logger = logging.getLogger("my_logger")

def foo():
    logger.error("ERROR")