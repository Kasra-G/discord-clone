<script>
	import { getUserState } from './user.svelte';

	const { children } = $props();
</script>

{#if getUserState().authenticated()}
	{@render children()}
{:else}
	Unauthorized
{/if}
