<script lang="ts">
	import { resolve } from '$app/paths';
	import { page } from '$app/state';
	import { createChannel, getChannels } from '$lib/backend.remote';
	import { DEFAULT_GUILD } from '$lib/const';
	import * as schemas from '$lib/schemas';
	import type { Channel } from '$lib/model';
	import Modal from './Modal.svelte';

	let guildId = $derived(page.params.guildId!);

	let channels = $derived(await getChannels({ guildId }));
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

<Modal bind:showModal>
	<form
		{...createChannel.preflight(schemas.CREATE_CHANNEL)}
		{...createChannel.enhance(async (form) => {
			if (await form.submit()) {
				form.element.reset();
				showModal = false;
			}
		})}
		style="color: var(--foreground);"
	>
		<label>
			Channel name <input {...createChannel.fields.channelName.as('text')} />
		</label>
		<label>
			Channel description <input {...createChannel.fields.channelDescription.as('text')} />
		</label>
		<input {...createChannel.fields.guildId.as('hidden', guildId)} />
		<button>Submit</button>
	</form>
</Modal>

<div class="root">
	<h3 class="text-overflow">JuanDaSwancord</h3>
	<div class="channel-group">
		<span class="channel-group-name text-overflow">Text Channels</span>
		<button
			class="channel-group-add"
			onclick={() => {
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
