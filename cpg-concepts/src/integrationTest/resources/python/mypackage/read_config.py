import configparser

config = configparser.ConfigParser()
config.read('config.ini')

default = config['DEFAULT']
port = default['port']

ssl_enabled = config['ssl']['enabled']

print(port)
print(ssl_enabled)
