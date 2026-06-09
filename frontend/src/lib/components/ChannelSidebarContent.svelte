<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { getChannels } from '$lib/backend.remote';
	import { DEFAULT_GUILD } from '$lib/const';
	import type { Channel } from '$lib/model';
	import Modal from './Modal.svelte';

	let channels = $derived(await getChannels({ guildId: page.params.guildId! }));
	let showModal = $state(false);
</script>

{#snippet channelButton(channel: Channel)}
	<div class="channel-row-wrapper">
		<a
			class="channel-link"
			href={resolve(`/channels/${DEFAULT_GUILD}/${channel.id}`)}
			class:selected={page.params.channelId === channel.id}
		>
			<span>#</span>
			<span class="text-overflow">{channel.name}</span>
		</a>
	</div>
{/snippet}

<Modal bind:showModal>Aye</Modal>

<div class="root">
	<h3 class="text-overflow">JuanDaSwancord</h3>
	<div class="channel-group">
		<span class="channel-group-name text-overflow">Text Channels</span>
		<button
			class="channel-group-add"
			onclick={() => {
				console.log('yo');
				showModal = true;
			}}>+</button
		>
	</div>
	<div class="channel-list">
		{#each channels as channel (channel.id)}
			{@render channelButton(channel)}
		{/each}
	</div>
</div>

<style>
	.channel-group {
		color: var(--muted-foreground);
		display: flex;
		align-items: center;
		cursor: pointer;

		.channel-group-name {
			font-size: smaller;
			flex-grow: 1;
		}

		.channel-group-add {
			font-size: x-large;
			color: inherit;
			background: inherit;
			border: inherit;
			cursor: inherit;
		}
		.channel-group-add:hover {
			color: var(--accent-foreground);
		}

		.channel-group-name:hover {
			color: var(--accent-foreground);
		}
	}
	.root {
		height: 100%;
		margin: 4px;
		padding: 0 8px 16px 8px;
		display: flex;
		flex-direction: column;
		overflow: hidden;
	}

	.channel-row-wrapper {
		width: 100%;
	}

	h3 {
		margin: 12px 8px;
		flex-shrink: 0;
		color: var(--foreground);
	}

	.channel-list {
		display: flex;
		flex-direction: column;
		gap: 4px;
		flex-grow: 1;
		overflow-y: auto;
	}

	.channel-link {
		display: flex;
		align-items: center;
		gap: 4px;
		padding: 6px 8px;
		border-radius: 8px;
		transition: background-color 0.1s ease;
	}

	.text-overflow {
		text-overflow: ellipsis;
		white-space: nowrap;
		overflow: hidden;
	}

	a {
		display: block;
		text-decoration: none;
		color: var(--muted-foreground);
	}
	a:hover {
		background-color: var(--accent);
		color: var(--accent-foreground);
	}
	a.selected {
		background-color: var(--accent);
		color: var(--foreground);
		font-weight: 500;
	}
</style>
