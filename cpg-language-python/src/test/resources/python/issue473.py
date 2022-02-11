if sys.version_info.minor > 9:
    phr = {"user_id": user_id} | content
else:
    z = {"user_id": user_id}
    phr = {**z, **content}
