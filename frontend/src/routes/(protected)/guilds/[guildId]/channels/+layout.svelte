<script lang="ts">
	import { resolve } from '$app/paths';
	import { DEFAULT_GUILD } from '$lib/const';
	import { websocketService } from '$lib/websocket.svelte';

	interface Channel {
		readonly name: string;
		readonly id: string;
	}

	let { children } = $props();

	let channels = $state([
		{
			name: 'default',
			id: 'default'
		},
		{
			name: 'gaming',
			id: 'gaming'
		}
	]);
</script>

{#snippet channelButton(channel: Channel)}
	<form
		class="channel-entry-container"
		method="GET"
		action={resolve(`/guilds/${DEFAULT_GUILD}/channels/${channel.id}`)}
	>
		<button class="channel-entry">#{channel.name}</button>
	</form>
{/snippet}

<div class="wrapper">
	<div class="side-panel">
		<h3>JuanDaSwancord {websocketService.status}</h3>
		<div class="channel-list">
			{#each channels as channel (channel.id)}
				{@render channelButton(channel)}
			{/each}
		</div>
	</div>
	<div class="channel-wrapper">{@render children()}</div>
</div>

<style>
	.channel-entry {
		width: 100%;
		font-size: medium;
		text-align: left;
		padding-left: 10px;
	}
	.channel-entry-container {
		display: flex;
	}
	.channel-list {
		display: flex;
		flex-direction: column;
	}
	.side-panel {
		width: 200px;
		padding-left: 10px;
		padding-right: 5px;
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
