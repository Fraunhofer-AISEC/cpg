<!-- This is a simple component that is used to test the CPG language support for Svelte. -->
<script lang="ts">
	// Imports
	import { onMount } from 'svelte';

	// Props
	export let name: string = "World";

	// Variables
	let count: number = 0;

	// Methods ???
	onMount(() => {
		console.log('SimpleComponent mounted');
	});

	// Functions
	function handleClick() {
		count += 1;
	}
</script>

<!-- HTML -->
<div>
	<h1>Hello {name}!</h1>
	<p>You've clicked the button {count} times.</p>
	<button on:click={handleClick}>
		Click me
	</button>
</div>

<!-- CSS -->
<style>
	h1 {
		color: purple;
	}
</style> 