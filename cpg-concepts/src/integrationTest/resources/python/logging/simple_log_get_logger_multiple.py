import logging


loggerFoo1 = logging.getLogger('foo')
loggerFoo2 = logging.getLogger('foo')
loggerBar = logging.getLogger('bar')
loggerDefault1 = logging.getLogger()
loggerDefault2 = logging.getLogger(None)
loggerDefault3 = logging.getLogger('')

# have only one occurrence of each literal to make testing easier
default_logger_name = 'default logger'
foo_logger_name = 'foo logger'
bar_logger_name = 'bar logger'


loggerFoo1.fatal(foo_logger_name)
loggerFoo2.debug(foo_logger_name)
loggerBar.info(bar_logger_name)
loggerDefault1.warn(default_logger_name)
loggerDefault2.warning(default_logger_name)
loggerDefault3.error(default_logger_name)
logging.critical(default_logger_name)
