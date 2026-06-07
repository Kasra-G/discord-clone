<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { getChannels } from '$lib/backend.remote';
	import { DEFAULT_GUILD } from '$lib/const';
	import type { Channel } from '$lib/model';
	import { getUserState } from '$lib/user.svelte';
	import { websocketService } from '$lib/websocket.svelte';

	let { params, children } = $props();

	let channels = $derived(await getChannels({ guildId: params.guildId }));
	let draggingSidepanel = $state(false);
	let sidepanelWidth = $state(200);
	let sidepanel: HTMLDivElement;
</script>

{#snippet channelButton(channel: Channel)}
	<div class="channel-entry-container">
		<a
			class="channel-entry text-overflow"
			href={resolve(`/guilds/${DEFAULT_GUILD}/channels/${channel.id}`)}
			class:selected={page.params.channelId === channel.id}>#{channel.name}</a
		>
	</div>
{/snippet}

<div class="wrapper">
	<div class="side-panel" style:width={`${sidepanelWidth}px`} bind:this={sidepanel}>
		<h3 class="text-overflow">JuanDaSwancord</h3>
		<div class="channel-list">
			{#each channels as channel (channel.id)}
				{@render channelButton(channel)}
			{/each}
		</div>
		<div class="side-panel-profile">{getUserState().current?.username}</div>
	</div>

	<div
		class="side-panel-slider"
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
			sidepanelWidth = Math.max(0, e.clientX - sidepanel.getBoundingClientRect().left - 2);
		}}
	></div>
	<div class="channel-wrapper">{@render children()}</div>
</div>

<style>
	.channel-entry-container {
		display: flex;
	}

	.channel-entry:hover {
		color: var(--text-normal);
	}
	.channel-entry {
		border-radius: 5px;
		width: 100%;
		font-size: medium;
		text-align: left;
		padding: 4px 10px;
		color: var(--text-muted);
		text-decoration: none;
	}

	.text-overflow {
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.selected {
		background-color: var(--background-secondary);
		color: var(--text-normal);
	}
	.channel-list {
		display: flex;
		flex-direction: column;
	}
	.side-panel-profile {
		margin-top: auto;
		text-align: center;
		margin-bottom: 5px;
		padding: 15px;
		border: 5px solid var(--background-secondary);
		border-radius: 20px;
	}
	.side-panel {
		display: flex;
		box-sizing: border-box;
		flex-direction: column;
		min-width: 100px;
		max-width: 300px;
		padding-inline: 5px;
	}
	.side-panel-slider {
		width: 5px;
		background-color: var(--background-secondary);
		cursor: col-resize;
	}
	.dragging {
		background-color: var(--background-accent);
	}
	.channel-wrapper {
		display: flex;
		flex-grow: 1;
	}
	.wrapper {
		display: flex;
		height: 100%;
	}
</style>
