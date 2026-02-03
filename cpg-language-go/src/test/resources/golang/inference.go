package p

import "database/sql"

func doDB() {
	var db *sql.DB
	db.Query("SELECT * FROM table")
}
