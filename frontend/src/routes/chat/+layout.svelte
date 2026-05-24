<script lang="ts">
	import { commandService } from '$lib/command-service.svelte';
	import { websocketService } from '$lib/websocket.svelte';

	$effect(() => {
		websocketService.connect();
		commandService.loadMessages();
	});

	let { children } = $props();
</script>

{@render children()}
