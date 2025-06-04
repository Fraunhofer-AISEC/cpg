// This is a simple class component that is used to test the CPG language support for React.

// Imports
import { Component } from 'react';

// Props
interface SimpleComponentProps {
	name?: string;
}

interface SimpleComponentState {
	count: number;
}

export class SimpleComponent extends Component<SimpleComponentProps, SimpleComponentState> {
	// Props default values
	static defaultProps = {
		name: "World"
	};

	// State (similar to Svelte variables)
	state: SimpleComponentState = {
		count: 0
	};

	// Methods (similar to Svelte lifecycle)
	componentDidMount() {
		console.log('SimpleComponent mounted');
	}

	// Functions (similar to Svelte functions)
	handleClick = () => {
		this.setState({ count: this.state.count + 1 });
	}

	// Render method (similar to Svelte HTML)
	render() {
		const { name } = this.props;
		const { count } = this.state;
		
		return (
			<div>
				<h1 style={{ color: 'purple' }}>Hello {name}!</h1>
				<p>You've clicked the button {count} times.</p>
				<button onClick={this.handleClick}>
					Click me
				</button>
			</div>
		);
	}
}