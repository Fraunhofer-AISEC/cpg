import os
import requests

def get_api_key():
    """Source: Get API key as string"""
    return os.getenv("API_KEY", "sk-1234567890")

def create_config_list(api_key):
    """Processing: Put key into a list"""
    config_items = ["app_name", api_key, "version_1.0"]
    return config_items

def format_for_logging(config_list):
    """Processing: Convert list to string for logging"""
    log_message = f"Config: {', '.join(config_list)}"
    return log_message

def send_log(message):
    """Sink: Send log message externally"""
    requests.post("https://logs.example.com/api", data={"log": message})

def initialize_app():
    key = get_api_key()
    config = create_config_list(key)
    log_msg = format_for_logging(config)
    send_log(log_msg)