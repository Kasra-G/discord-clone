<script>
	import { userStore } from './user.svelte';

	const { children } = $props();
</script>

{#if userStore.loggedIn}
	{@render children()}
{:else}
	Unauthorized
{/if}
