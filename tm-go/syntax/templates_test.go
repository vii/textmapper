package syntax_test

import (
	"fmt"
	"strings"
	"testing"

	"github.com/inspirer/textmapper/tm-go/syntax"
	"github.com/inspirer/textmapper/tm-go/util/dump"
)

var modelTests = []struct {
	fnName string
	fn     func(m *syntax.Model) error
	input  string
	want   string
}{
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: C; C: [V] d;`,
		`%flag V; Z: A<V=true>; A<V>: B<V=V>; B<V>: C<V=V>; C<V>: [V] d;`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: (C | c); C: [V] d;`,
		`%flag V; Z: A<V=true>; A<V>: B<V=V>; B<V>: C<V=V> | c; C<V>: [V] d;`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%flag P; %lookahead flag V; Z<P>: a A; A: B<V=true>; B: C; C: d Z<P=V>;`,
		`%flag P; %flag V; Z<P>: a A; A: B<V=true>; B<V>: C<V=V>; C<V>: d Z<P=V>;`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: C? c; C: [V] d;`,
		`ERR: input:1:40: cannot propagate lookahead flag V through nonterminal B; avoid nullable alternatives and optional clauses`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: (C | c)?; C: [V] d;`,
		`ERR: input:1:40: cannot propagate lookahead flag V through nonterminal B; avoid nullable alternatives and optional clauses`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: (C | c)*; C: [V] d;`,
		`ERR: input:1:40: cannot propagate lookahead flag V through nonterminal B; avoid nullable alternatives and optional clauses`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: (C | c)+; C: [V] d;`,
		`%flag V; Z: A<V=true>; A<V>: B<V=V>; B<V>: (C<V=V> | c)+; C<V>: [V] d;`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: (C | C<V=false> c)+; C: [V] d;`,
		`%flag V; Z: A<V=true>; A<V>: B<V=V>; B<V>: (C<V=V> | C<V=false> c)+; C<V>: [V] d;`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: (C | c?); C: [V] d;`,
		`ERR: input:1:40: cannot propagate lookahead flag V through nonterminal B; avoid nullable alternatives and optional clauses`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B; B: C; C: d;`,
		`ERR: input:1:25: V is not used in A`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A<V=true>; A: B<V=true>; B: C; C: [V] d;`,
		`ERR: input:1:25: V is not used in A`,
	},
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V;Z2:[V] A; A: B<V=true>; B: C; C: [V] d;`,
		`ERR: input:1:23: lookahead flag V is never provided`,
	},
	// Lookahead flag arguments in token sets.
	{"PropagateLookaheads", syntax.PropagateLookaheads,
		`%lookahead flag V; Z: A set(B<V=true>); A: c; B: C; C: [V] d;`,
		`%flag V; Z: A set(B<V=true>); A: c; B<V>: C<V=V>; C<V>: [V] d; `,
	},

	// Template instantiations.
	{"Instantiate", syntax.Instantiate,
		`%input Z; %flag V; Z: A set(B<V=true>); A: c; B<V>: C<V=V>; C<V>: [V] d; F2: c;`,
		`%input Z; Z: A set(B_V); A: c; B_V: C_V; C_V: d;`,
	},
	{"Instantiate", syntax.Instantiate,
		`%input Z; %flag V; Z: b B<V=true> | c B<V=false>; B<V>: [V] a | b;`,
		`%input Z; Z: b B_V | c B; B: b; B_V: a | b;`,
	},
	{"Instantiate", syntax.Instantiate,
		`%input Z; %flag V; %flag T; Z: b B<V=true> | c B<V=false>; B<V>: [V] a | Q<T=V>; Q<T>: [!T] n | [T] t;`,
		`%input Z; Z: b B_V | c B; B: Q; B_V: a | Q_T; Q: n; Q_T: t;`,
	},
	{"Instantiate", syntax.Instantiate,
		`%input Z; %flag A; %flag B;
       Z: F<A=true,B=true> | F<A=true,B=false> | F<A=false,B=true> | F<A=false, B=false>;
       F<A,B>: [A && B] n a b | [A] a | [B] b | [!A && !B] n;`,
		`%input Z; Z: F_A_B | F_A | F_B | F; F: n; F_A: a; F_A_B: n a b | a | b; F_B: b;`,
	},
}

func TestModelTransforms(t *testing.T) {
	for _, tc := range modelTests {
		model, err := parse(tc.input)
		if err != nil {
			t.Errorf("cannot parse %q: %v", tc.input, err)
			continue
		}

		if err := tc.fn(model); err != nil {
			const prefix = "ERR: "
			if !strings.HasPrefix(tc.want, prefix) {
				t.Errorf("%v(%v) failed with %v", tc.fnName, tc.input, err)
				continue
			}
			if got := fmt.Sprintf("ERR: %v", err); got != tc.want {
				t.Errorf("%v(%v) failed with %v, want: %v", tc.fnName, tc.input, got, tc.want)
			}
			continue
		}

		want, err := parse(tc.want)
		if err != nil {
			t.Errorf("cannot parse %q: %v", tc.want, err)
			continue
		}

		stripSelfRef(model)
		stripOrigin(model)
		stripOrigin(want)
		if diff := dump.Diff(want, model); diff != "" {
			t.Errorf("%v(%v) produced diff (-want +got):\n%s", tc.fnName, tc.input, diff)

		}
	}
}