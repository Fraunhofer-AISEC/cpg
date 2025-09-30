from pw_manager_db import get_db, close_db
from flask import Flask, jsonify, request
import hmac
import binascii
from flask import jsonify, request
import requests
from google.oauth2 import service_account
from google.auth.transport.requests import Request
import subprocess
import json


app = Flask(__name__)
app.teardown_appcontext(close_db)

@app.get("/ping")
def ping():
    return jsonify(status="ok", message="password-manager api is on")

@app.get("/users")
def list_all_users():
    db = get_db()
    rows = db.execute("SELECT * FROM users").fetchall()

    count = db.execute("SELECT COUNT(*) FROM users").fetchone()[0]
    print("user.count= ddd", count)

    data = []
    for r in rows:
        user_dict = dict(r)
        data.append((user_dict))

    return data

@app.post("/users")
def create_user():
    data = request.get_json(force=True)  # JSON Request holen

    username = data.get("username")
    password = data.get("password")
    salt = data.get("salt")
    fernet_key = data.get("fernet_key")
    if not username or not password or not salt:
        return {"error": "username and password"}

    db = get_db()
    db.execute("INSERT INTO users (username, password, salt, fernet_salt) VALUES (?, ?, ?, ?)",(username, password, salt, fernet_key))
    db.commit()

    return {"created": True, "username": username}



@app.get("/entry")
def list_all_entries():
    db = get_db()
    rows = db.execute("SELECT id, user_id, site, username, password FROM entries").fetchall()
    return jsonify([dict(r) for r in rows]), 200

@app.get("/entry/<int:user_id>")
def list_entries_for_user(user_id):
    db = get_db()
    rows = db.execute(
        "SELECT id, user_id, site, username, password FROM entries WHERE user_id = ?",(user_id,)).fetchall()
    return jsonify([dict(r) for r in rows]), 200

@app.post("/add_entry")
def add_entrie():
    data = request.get_json(force=True)
    userid = data.get("id")
    site = data.get("site")
    username = data.get("username")
    password = data.get("password")

    db = get_db()
    db.execute("INSERT INTO entries (user_id, site, username, password) VALUES (?, ?, ?, ?)", (userid, site, username, password))
    db.commit()
    return {"add entrie": True, "userid": userid, "site": site, "username": username, "password": password}

@app.post("/del_entry")
def del_entry():
    data = request.get_json(force=True)
    entry_id = data.get("id")
    user_id = data.get("user_id")
    db = get_db()

    db.execute("DELETE FROM entries WHERE id = ? AND user_id = ?", (entry_id, user_id))
    db.commit()
    return {"deleted": True, "id": entry_id}

@app.post("/del_user")
def del_user():
    data = request.get_json(force=True)
    user_id = data.get("user_id")
    db = get_db()
    db.execute("DELETE FROM users WHERE id = ?", (user_id,))
    db.commit()
    return {"deleted": True, "id": user_id}

@app.post("/del_user_entry")
def del_user_entry():
    data = request.get_json(force=True)
    user_id = data.get("user_id")
    db = get_db()
    db.execute("DELETE FROM entries WHERE user_id = ?", (user_id,))
    db.commit()


@app.post("/salt")
def get_salt():
    data = request.get_json(force=True) or {}
    username = (data.get("username") or "").strip()
    if not username:
        return jsonify(error="username required"), 400

    db = get_db()
    row = db.execute("SELECT salt, fernet_salt FROM users WHERE username = ?",(username,)).fetchone()
    if row is None:
        return jsonify(ok=False, message="invalid credentials"), 401
    fernet_key = row["fernet_salt"]
    salt_value = row["salt"]
    # in DB als HEX gespeichert -> zurückgeben
    if isinstance(salt_value, str):
        return jsonify(ok=True, salt=salt_value, fernet_key=fernet_key), 200
    #Bytes gespeichert -> in HEX wandeln
    return jsonify(ok=True, salt=binascii.hexlify(salt_value).decode()), 200


@app.post("/login")
def login_simple():
    data = request.get_json(force=True) or {}
    username = (data.get("username") or "").strip()
    client_hash_hex = data.get("client_hash")  # PBKDF2 vom Client (HEX)

    if not username or not client_hash_hex:
        return jsonify(error="username and client_hash required")

    db = get_db()
    row = db.execute("SELECT id, username, password FROM users WHERE username = ?",(username,)).fetchone()

    if row is None:
        return jsonify(ok=False, message="invalid credentials"), 401

    # DB: password ist ein HEX-String -> in Bytes wandeln
    stored_hex = row["password"]
    stored_hash = binascii.unhexlify(stored_hex) if isinstance(stored_hex, str) else stored_hex

    try:
        client_hash = binascii.unhexlify(client_hash_hex)
    except binascii.Error:
        return jsonify(error="invalid client_hash format"), 400

    if not hmac.compare_digest(stored_hash, client_hash):
        return jsonify(ok=False, message="invalid credentials"), 401

    return jsonify(ok=True, user_id=row["id"], username=row["username"]), 200


@app.post("/google_analytics")
def rcv_google_analytics():

    MEASUREMENT_ID = "G-3BBEQVSKQ7"
    API_SECRET = "cT3ol8-mTKiNSOAql8y8DA"

    data = request.get_json(silent=True)
    username = (data.get("username") or "")
    user_id = data.get("user_id")

    if not username or not user_id:
        return jsonify(ok=False, error="username oder user_id fehlt"), 400

    url = f"https://www.google-analytics.com/mp/collect?measurement_id={MEASUREMENT_ID}&api_secret={API_SECRET}"

    payload = {
        "client_id": str(user_id),
        "user_id": str(user_id),
        "events": [{
            "name": "user_logged_in",
            "params": {
                "username": username,
                "debug_mode": 1
            }
        }]
    }

    try:
        r = requests.post(url, json=payload, timeout=10)
        ok = (r.status_code == 204)
        return jsonify(
            ok=ok,
            privacy={"username_sent_to_ga4": True},
            send_event={"ok": ok, "http_status": r.status_code},
            echo={
                "user_id": str(user_id),
                "username": username}), 200
    except Exception as e:
        return jsonify(
            ok=False,
            privacy={"username_sent_to_ga4": False},
            send_event={"ok": False, "http_status": None, "error": str(e)},
            echo={
                "user_id": str(user_id),
                "username": username}), 500

@app.post("/ga4_rcv")
def ga4_rcv():

    PROPERTY_ID = "504744953"
    KEY_FILE = "service-account.json"

    SCOPES = ["https://www.googleapis.com/auth/analytics.readonly"]
    credentials = service_account.Credentials.from_service_account_file(KEY_FILE, scopes=SCOPES)

    if not credentials.valid:
        credentials.refresh(Request())
    access_token = credentials.token

    payload = {
        "dateRanges": [{"startDate": "7daysAgo", "endDate": "today"}],
        "metrics": [{"name": "eventCount"}],
        "dimensions": [{"name": "eventName"}, {"name": "customEvent:username"}]
    }

    headers = {"Authorization": f"Bearer {access_token}","Content-Type": "application/json"}

    r = requests.post(f"https://analyticsdata.googleapis.com/v1beta/properties/{PROPERTY_ID}:runReport", headers=headers, json=payload, timeout=10)
    data = r.json()

    simple_rows = []
    for row in data.get("rows", []):
        event = row["dimensionValues"][0]["value"]
        uname = row["dimensionValues"][1]["value"]
        cnt   = row["metricValues"][0]["value"]
        simple_rows.append({"eventName": event, "username": uname, "eventCount": cnt})

    return (jsonify(ok=True, ga4=data, rows=simple_rows))

@app.post("/ga4_curl")
def curl():
    PROPERTY_ID = "504744953"
    KEY_FILE = "service-account.json"

    SCOPES = ["https://www.googleapis.com/auth/analytics.readonly"]
    credentials = service_account.Credentials.from_service_account_file(KEY_FILE, scopes=SCOPES)
    if not credentials.valid:
        credentials.refresh(Request())
    access_token = credentials.token

    payload = json.dumps({
        "dateRanges": [{"startDate": "7daysAgo", "endDate": "today"}],
        "metrics": [{"name": "eventCount"}],
        "dimensions": [{"name": "eventName"}, {"name": "customEvent:username"}]
    })

    cmd = [
        "curl",
        "-s",
        "-X", "POST",
        "-H", f"Authorization: Bearer {access_token}",
        "-H", "Content-Type: application/json",
        f"https://analyticsdata.googleapis.com/v1beta/properties/{PROPERTY_ID}:runReport",
        "-d", payload
    ]

    result = subprocess.run(cmd, capture_output=True, text=True)
    data = json.loads(result.stdout)

    simple_rows = []
    for row in data.get("rows", []):
        event = row["dimensionValues"][0]["value"]
        uname = row["dimensionValues"][1]["value"]
        cnt   = row["metricValues"][0]["value"]
        simple_rows.append({"eventName": event, "username": uname, "eventCount": cnt})

    return jsonify(ok=True, ga4=data, rows=simple_rows)

# Server starten
if __name__ == "__main__":
    app.run(debug=True)