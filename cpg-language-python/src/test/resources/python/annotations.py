from flask import Flask, request

app = Flask(__name__)


@app.route("/data", methods=['POST'])
def collect_data():
    return "OK", 200


@some.otherannotation
def other_func(func):
    pass


@other_func
def other_other_func():
    pass


if __name__ == '__main__':
    app.run(host='0.0.0.0', port=8080, debug=True, threaded=True)
