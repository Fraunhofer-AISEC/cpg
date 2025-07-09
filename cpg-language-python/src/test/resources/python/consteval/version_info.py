import sys

if sys.version_info < (3, 14):
    def below_3_14(): pass
else:
    def should_not_be_reachable(): pass

if sys.version_info <= (3, 14):
    def below_or_equal_3_14(): pass
else:
    def should_not_be_reachable(): pass

if sys.version_info > (3, 10):
    def greater_3_10(): pass
else:
    def should_not_be_reachable(): pass

if sys.version_info >= (3, 10):
    def greater_or_equal_3_10(): pass
else:
    def should_not_be_reachable(): pass

if sys.version_info == (3, 12):
    def equal_3_12(): pass
else:
    def should_not_be_reachable(): pass

if sys.version_info != (3, 14):
    def not_equal_3_14(): pass
else:
    def should_not_be_reachable(): pass
