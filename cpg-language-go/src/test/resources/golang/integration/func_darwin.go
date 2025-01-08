// Some other header

//go:build darwin

// Package buildtags is awesome
package buildtags

func OS() string {
	return "darwin"
}
