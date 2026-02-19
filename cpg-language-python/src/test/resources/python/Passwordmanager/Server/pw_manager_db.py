import sqlite3
from pathlib import Path
from flask import g, Flask

DB_PATH = Path(__file__).with_name("database.db")

app = Flask(__name__)

def get_db():
    if "db" not in g:
        g.db = sqlite3.connect(DB_PATH)
        g.db.row_factory = sqlite3.Row
        g.db.execute("PRAGMA foreign_keys = ON;")
        print("DB_PATH ->", DB_PATH.resolve())
    return g.db

@app.teardown_appcontext
def close_db(exception):
    db = g.pop("db", None)
    if db is not None:
        db.close()


def init_db():
    db = sqlite3.connect(DB_PATH)
    db.execute("PRAGMA foreign_keys = ON;")

    db.execute("""CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    fernetkey TEXT NOT NULL)""")

    db.execute("""CREATE TABLE IF NOT EXISTS entries(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    site TEXT NOT NULL,
                    username TEXT NOT NULL,
                    password TEXT NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE);""")

    db.commit()
    db.close()