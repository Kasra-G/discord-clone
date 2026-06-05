<script lang="ts">
	import { getMotd } from '$lib/backend.remote';
	import { websocketService } from '$lib/websocket.svelte';

	let { children } = $props();
</script>

<div class="wrapper">
	<h3>Message of the Day: {await getMotd()}</h3>

	<h3>Server status: {websocketService.status}</h3>
	{@render children()}
</div>

<style>
	.wrapper {
		display: flex;
		flex-direction: column;
		height: 100%;
	}
</style>
