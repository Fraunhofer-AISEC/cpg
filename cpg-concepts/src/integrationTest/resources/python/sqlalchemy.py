def foo(app):
    secret = getSecret()
    db = SQLAlchemy(app)
    db.session.add(42)
