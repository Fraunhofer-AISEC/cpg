import configparser
import os

if os.environ.get('PROD') == 'true':
    config_file = 'prod.ini'
else:
    config_file = 'dev.ini'

config = configparser.ConfigParser()
config.read(config_file)

default = config['DEFAULT']
another_default = config['DEFAULT']
same_config = config
yet_another_default = same_config['DEFAULT']
port = default['port']

ssl_enabled = config['ssl']['enabled']

print(port)
print(ssl_enabled)
