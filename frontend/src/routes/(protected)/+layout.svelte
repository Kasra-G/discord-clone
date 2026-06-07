<script lang="ts">
	import ChannelSidebarContent from '$lib/components/ChannelSidebarContent.svelte';
	import ServerSidebar from '$lib/components/ServerSidebar.svelte';
	import { getUserState } from '$lib/user.svelte';
	import { websocketService } from '$lib/websocket.svelte';
	import { onMount } from 'svelte';

	let { children } = $props();

	onMount(() => {
		websocketService.connect();
	});
	let draggingSidepanel = $state(false);
	let sidepanelWidth = $state(240);
	let sidepanel: HTMLDivElement;
</script>

<div class="base" style={`grid-template-columns: 72px ${sidepanelWidth}px 1fr;`}>
	<div class="server-sidebar-background"></div>
	<ServerSidebar class="server-sidebar" />
	<div class="sidebar-background" bind:this={sidepanel}></div>
	<div
		class="resizer"
		role="separator"
		class:dragging={draggingSidepanel}
		onpointerdown={(e) => {
			e.currentTarget.setPointerCapture(e.pointerId);
			draggingSidepanel = true;
		}}
		onpointerup={(e) => {
			e.currentTarget.releasePointerCapture(e.pointerId);
			draggingSidepanel = false;
		}}
		onpointermove={(e) => {
			if (!draggingSidepanel || !sidepanel) return;
			e.preventDefault();
			sidepanelWidth = Math.max(160, Math.min(400, e.clientX - 72 - 2));
		}}
	></div>
	<div class="sidebar-content"><ChannelSidebarContent /></div>
	<div class="sidebar-profile">{getUserState().current?.username}</div>

	<main class="content">
		{@render children()}
	</main>
</div>

<style>
	.content {
		grid-column: 3;
		grid-row: 1/-1;
		min-width: 0;
		overflow: hidden;
	}
	.base {
		display: grid;
		height: 100vh;
		width: 100vw;
		overflow: hidden;
		grid-template-rows: 1fr 72px;
	}

	.server-sidebar-background {
		grid-column: 1;
		grid-row: 1 / -1;
		background-color: var(--sidebar);
	}

	:global(.server-sidebar) {
		grid-column: 1;
		grid-row: 1;
	}

	.sidebar-background {
		grid-row: 1 / -1;
		grid-column: 2;
		background-color: var(--card);
	}

	.sidebar-content {
		grid-row: 1;
		grid-column: 2;
		overflow: hidden;
	}

	.sidebar-profile {
		margin-top: auto;
		background-color: var(--secondary);
		text-align: center;
		margin: 8px;
		padding: 12px;
		grid-row: 2;
		grid-column: 1 / 3;
		border: 1px solid var(--accent);
		border-radius: 12px;
	}

	.resizer {
		grid-column: 3;
		width: 4px;
		margin-left: -2px;
		grid-row: 1/-1;
		cursor: col-resize;
		position: relative;
		z-index: 10;
	}

	.resizer::after {
		content: '';
		position: absolute;
		top: 0;
		bottom: 0;
		left: 1px;
		right: 1px;
		background-color: var(--border);
		transition:
			background-color 0.5s ease,
			width 0.5s ease;
	}

	.resizer:hover::after,
	.resizer.dragging::after {
		width: 2px;
		background-color: var(--primary);
	}
</style>
