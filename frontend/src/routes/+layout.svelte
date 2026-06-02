<script lang="ts">
	import favicon from '$lib/assets/favicon.svg';
	import { userStore } from '$lib/user.svelte';

	let { children } = $props();
	userStore.initialize();
</script>

<svelte:head>
	<link rel="icon" href={favicon} />
</svelte:head>

{@render children()}
