package liner

type State struct{ n int }

func NewLiner() *State { return &State{} }

func (s *State) AppendHistory(item string) { s.n = appended(s.n) }
func (s *State) Close() error              { return nil }

func appended(n int) int { return n + 1 }
