<script>
	import { getUserState } from '$lib/user.svelte';

	const { children } = $props();
</script>

{#if getUserState().authenticated()}
	{@render children()}
{:else}
	Unauthorized
{/if}
