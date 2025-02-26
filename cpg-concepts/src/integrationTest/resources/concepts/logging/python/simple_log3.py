import logging as log

log.info("INFO")
log.error("123")
logger = log.getLogger(__name__)
logger.error('ERROR')
