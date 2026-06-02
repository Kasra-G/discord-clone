<script lang="ts">
	import { commandService } from '$lib/command-service.svelte';
	import { websocketService } from '$lib/websocket.svelte';
	import { onMount } from 'svelte';

	onMount(() => {
		commandService.loadMessages();
		websocketService.connect();
	});

	let { children } = $props();
</script>

{@render children()}
