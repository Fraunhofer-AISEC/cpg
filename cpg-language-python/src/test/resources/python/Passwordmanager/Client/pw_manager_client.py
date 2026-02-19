import requests
import hashlib
import secrets
import binascii
from rapidfuzz import fuzz
from cryptography.fernet import Fernet

# @PersonalData
CURRENT_USER_ID = None
# @PersonalData
CURRENT_USERNAME = None

def menue():
    while True:
        print("-------------- Passwordmanager --------------")
        print("1) Login")
        print("2) Reg")
        print("3) Einträge lesen")
        print("4) Nach Eintrag suchen")
        print("5) Eintrag löschen")
        print("6) User löschen")
        print("7) Eintrag hinzufügen")
        print("8) Google Analytics SEND")
        print("9) Google Analytics RCV")
        print("10) Google Analytics RCV CURL")

        inp = input()

        match inp:
            case "1":
                log()
            case "2":
                reg()
            case "3":
                read_all()
            case "4":
                search_entry()
            case "5":
                delete_entry()
            case "6":
                delete_user()
            case "7":
                add_entry()
            case "8":
                google_analytics()
            case "9":
                ga4_rcv()
            case "10":
                ga4_rcv_curl()


def reg():
    print("\n -------------- Registrieren --------------\n")
    user = input("Benutzername: ")
    salt_bytes = secrets.token_bytes(16) # Salt erstellen
    password = input("Passwort: ")



    _pw = derive_pbkdf2(password, salt_bytes)
    hash_hex = binascii.hexlify(_pw).decode()
    salt_hex = binascii.hexlify(salt_bytes).decode()

    fernet_key = Fernet.generate_key().decode()

    r = requests.post("http://127.0.0.1:5000/users",json={"username": user, "password": hash_hex, "salt": salt_hex, "fernet_key": fernet_key})


def add_entry():
    if CURRENT_USER_ID is None:
        print("Bitte zuerst einloggen.")
        return
    site = input("Seite eingeben: ")
    name = input("Benutzer eingeben: ")
    password = input("Passwort: ")

    fernet = Fernet(FERNET_KEY)
    encrypted_pw = fernet.encrypt(password.encode()).decode()

    r = requests.post("http://127.0.0.1:5000/add_entry",json={"id": CURRENT_USER_ID, "site": site, "username": name, "password": encrypted_pw})
    print(r.json())

def read_all():
    if CURRENT_USER_ID is None:
        print("Bitte zuerst einloggen.")
        return

    url = f"http://127.0.0.1:5000/entry/{CURRENT_USER_ID}"
    r = requests.get(url)


    try:
        data = r.json()
    except Exception:
        print("Antwort war kein JSON:", r.status_code, r.text)
        return

    print("-------------- Einträge --------------")
    if not data:
        print("(keine Einträge)")
        return
    fernet = Fernet(FERNET_KEY)
    for e in data:
        decry_pw = fernet.decrypt(e['password']).decode("utf-8") #entsch
        print(f"{e['id']:>3}  {e['site']}  {e['username']}  {decry_pw}")


def search_entry():
    if CURRENT_USER_ID is None:
        print("Bitte zuerst einloggen")
        return
    inp = input("Nach Eintrag in Seite oder Benutzername suchen: ").strip()
    r = requests.get(f"http://127.0.0.1:5000/entry/{CURRENT_USER_ID}")
    data = r.json()
    print("-------------- Einträge --------------")
    if not data:
        print("(keine Einträge)")
        return
    fernet = Fernet(FERNET_KEY)
    count = 0
    for e in data:
        decry_pw = fernet.decrypt(e['password']).decode("utf-8")
        wratiosite = fuzz.WRatio(inp.lower(),e["site"].lower())
        wratiousername = fuzz.WRatio(inp.lower(),e["username"].lower())
        print(f"wratiousername = {wratiousername}")
        print(f"wratiosite = {wratiosite}")
        if wratiosite > 60 or wratiousername > 60:
            print(f"{e['id']:>3}  {e['site']}  {e['username']}  {decry_pw}")
            count +=1
        elif inp.lower() in e["site"].lower() or inp.lower() in e["username"].lower():
            print(f"{e['id']:>3}  {e['site']}  {e['username']}  {decry_pw}")
            count +=1
        elif e["site"].lower().startswith(inp) or e["username"].lower().startswith(inp):
            print(f"{e['id']:>3}  {e['site']}  {e['username']}  {decry_pw}")
            count +=1
        elif e["site"].lower().endswith(inp) or e["username"].lower().endswith(inp):
            print(f"{e['id']:>3}  {e['site']}  {e['username']}  {decry_pw}")
            count += 1
    if count == 0:
        print("Keine Einträge gefunden")


def delete_entry():
    if CURRENT_USER_ID is None or CURRENT_USERNAME is None:
        print("Bitte erst anmelden")
        return
    inp = int(input("ID zum Löschen: "))
    answer = input(f"Wollen Sie wirklich den Eintrag mit der ID {inp} löschen? Y/N: ")
    if answer.lower() == "y":
        r = requests.post("http://127.0.0.1:5000/del_entry", json={"id": inp, "user_id": CURRENT_USER_ID})
        print(r.json())
    else:
        print("Fehler")




def delete_user():
    if CURRENT_USER_ID is None:
        print("Bitte erst anmelden")
        return

    answer = input(f"Wollen Sie wirklich den Benutzer mit der ID {CURRENT_USER_ID} löschen? Y/N: ")
    if answer.lower() == "y":
        inp = CURRENT_USER_ID
        r = requests.post("http://127.0.0.1:5000/del_user", json={"user_id": inp})
        print(r.json())
        r = requests.post("http://127.0.0.1:5000/del_entry", json={"user_id": inp})
        print(r.json())
    else:
        print("cancel")
        return

def log():
    global CURRENT_USER_ID, CURRENT_USERNAME, FERNET_KEY
    user = input("Benutzername: ").strip()
    password = input("Passwort: ").strip()

    r = requests.post("http://127.0.0.1:5000/salt", json={"username": user})
    data = r.json()
    if not data.get("ok"):
        print("Unbekannter Benutzer oder Fehler:", data)
        return

    FERNET_KEY = data["fernet_key"]

    salt_hex = data["salt"]
    salt_bytes = binascii.unhexlify(salt_hex)

    client_hash = derive_pbkdf2(password, salt_bytes)
    client_hash_hex = binascii.hexlify(client_hash).decode()

    r = requests.post("http://127.0.0.1:5000/login",json={"username": user, "client_hash": client_hash_hex})
    data = r.json()

    if data.get("ok"):
        CURRENT_USER_ID = data["user_id"]
        CURRENT_USERNAME = data["username"]
        print(f"Eingeloggt  als Username: {CURRENT_USERNAME}, FernetKey {FERNET_KEY}")
    else:
        print(f"Fehler {data}")

    print(data)


def derive_pbkdf2(password: str, salt_bytes: bytes) -> bytes:
    return hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt_bytes, 100000)

def google_analytics():
    if CURRENT_USERNAME is None:
        print("Bitte erst anmelden.")
        return

    try:
        r = requests.post("http://127.0.0.1:5000/google_analytics", json={"username": CURRENT_USERNAME, "user_id": CURRENT_USER_ID}, timeout=10)
        data = r.json()
    except Exception as e:
        print("Fehler beim Request:", e)
        return

    print("\n-------------- Google Analytics Ergebnis --------------")
    print(f"Angemeldeter Username: {CURRENT_USERNAME}")

    echo = data.get("echo", {})
    if echo:
        print(f"Echo user_id: {echo.get('user_id')}")
        print(f"Echo username: {echo.get('username')}")
    else:
        print("Kein Echo vom Server.")


    send_event = data.get("send_event", {})
    print(f"Event ok: {send_event.get('ok')}")
    print(f"HTTP-Status: {send_event.get('http_status')}")
    if send_event.get("error"):
        print(f"Fehler: {send_event.get('error')}")

def ga4_rcv():
    if CURRENT_USERNAME is None:
        print("Bitte erst anmelden.")
        return
    r = requests.post("http://127.0.0.1:5000/ga4_rcv")
    data = r.json()
    print("GA4 Daten:", data)

    for row in data.get("rows", []):
        print(f"{row['eventName']} ({row['username']}): {row['eventCount']}")

def ga4_curl():
    if CURRENT_USERNAME is None:
        print("Bitte erst anmelden.")
        return
    r = requests.post("http://127.0.0.1:5000/ga4_curl")
    data = r.json()
    print("GA4 Daten:", data)

    for row in data.get("rows", []):
        print(f"{row['eventName']} ({row['username']}): {row['eventCount']}")

if __name__ == "__main__":
    #logi()
    menue()