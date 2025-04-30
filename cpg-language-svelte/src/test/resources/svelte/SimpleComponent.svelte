<script>
    let count = 0;
    let name = 'world';
</script>

<button on:click={() => count++}>Click me</button>
<p>Count: {count}</p>

<h1>Hello {name}!</h1>

<style>
    button {
        background-color: #4CAF50;
        color: white;
        padding: 10px 20px;
        border: none;
        cursor: pointer;
    }

    button:hover {
        background-color: #45a049;
    }

    h1 {
        color: blue;
    }
</style>
