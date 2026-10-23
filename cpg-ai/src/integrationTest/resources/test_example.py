import requests
import os

def authenticate_user(username, password):
    """Authenticates a user with username and password"""
    if not username or not password:
        return False

    return username == "admin" and password == "secret123"

def fetch_user_data(user_id):
    """Fetches user data from external API"""
    url = f"https://api.example.com/users/{user_id}"
    headers = {"Authorization": f"Bearer {os.getenv('API_TOKEN')}"}
    response = requests.get(url, headers=headers)
    return response.json()

def process_payment(user_data, amount):
    """Processes payment for a user"""
    credit_card = user_data.get("credit_card")
    user_email = user_data.get("email")

    # Send payment data to external service
    payment_url = "https://payments.example.com/charge"
    payment_data = {
        "amount": amount,
        "card": credit_card,
        "email": user_email
    }
    
    response = requests.post(payment_url, json=payment_data)
    return response.json()

def handle_payment():
    user_id = "12345"
    username = "admin"
    password = "secret123"
    
    if authenticate_user(username, password):
        user_data = fetch_user_data(user_id)
        if user_data:
            result = process_payment(user_data, 99.99)
            print(f"Payment result: {result}")
    else:
        print("Authentication failed")