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
</script>

{#snippet channelButton(channel: Channel)}
	<form
		class="channel-entry-container"
		method="GET"
		action={resolve(`/guilds/${DEFAULT_GUILD}/channels/${channel.id}`)}
	>
		<button class="channel-entry" disabled={page.params.channelId === channel.id}
			>#{channel.name}</button
		>
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
		<div class="side-panel-profile">{getUserState().current?.username}</div>
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
	.side-panel-profile {
		margin-top: auto;
		text-align: center;
		margin-bottom: 5px;
		padding: 15px;
		border-width: 5px;
		border-radius: 20px;
		border-color: var(--background-secondary);
		border-style: solid;
	}
	.side-panel {
		display: flex;
		flex-direction: column;
		width: 200px;
		padding-left: 10px;
		padding-right: 5px;
		border-right: 2px solid var(--background-secondary);
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
