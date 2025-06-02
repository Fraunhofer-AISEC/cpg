<script lang="ts">
  export let name: string = "World";
  let count: number = 0;

  function handleClick() {
    count += 1;
  }
</script>

<h1>Hello {name}!</h1>
<p>You've clicked the button {count} {count === 1 ? 'time' : 'times'}.</p>
<button on:click={handleClick}>
  Click me
</button>

<style>
  h1 {
    color: purple;
  }
</style>