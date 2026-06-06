<script lang="ts">
	import { beforeNavigate } from '$app/navigation';
	import { getChannel, getMessages, sendMessage } from '$lib/backend.remote';
	import { messageService } from '$lib/message-service.svelte';
	import { deep } from '$lib/util.svelte.js';
	import { websocketService } from '$lib/websocket.svelte';
	import { onMount } from 'svelte';

	let { params } = $props();

	let inputField: HTMLElement;
	let autoscroll = $state(true);
	let messages = $derived(deep(await getMessages({ channelId: params.channelId, count: 100 })));

	beforeNavigate(({ to }) => {
		const channelId = to?.params?.channelId;
		if (channelId) {
			void getMessages({
				channelId: channelId,
				count: 100
			}).refresh();
		}
	});

	onMount(() => {
		return messageService.subscribeOnMessage((msg) => {
			if (msg.channelId === params.channelId) {
				messages.push(msg);
			}
		});
	});

	const autoscrollToLast = (elem: HTMLElement) => {
		if (messages.length <= 0) return;
		if (!autoscroll) return;
		elem.lastElementChild?.scrollIntoView();
	};

	const getChannelDetails = () => {
		return getChannel({ channelId: params.channelId });
	};
</script>

<div class="messages-container">
	<div class="header">
		<h4>#{(await getChannelDetails()).name}</h4>
		<h5>{(await getChannelDetails()).description}</h5>
	</div>
	<div
		class="chatbox-outer"
		onscroll={({ currentTarget }) => {
			autoscroll =
				currentTarget.scrollHeight - currentTarget.clientHeight - currentTarget.scrollTop < 1;
		}}
	>
		<div class="chatbox-inner" {@attach autoscrollToLast}>
			{#each messages as msg (msg.id)}
				<div class="message">
					<div class="message-header">
						<div class="message-username">
							{msg.author.username}
						</div>
						<div class="message-timestamp">
							{new Date(msg.createdAt).toLocaleString([], {
								dateStyle: 'short',
								timeStyle: 'short'
							})}
						</div>
					</div>
					<div>
						{msg.content}
					</div>
				</div>
			{/each}
		</div>
	</div>
	<form
		{...sendMessage.enhance(async (form) => {
			if (await form.submit().updates()) {
				form.element.reset();
				inputField.focus();
			}
		})}
		class="message-input-container"
	>
		<input
			{...sendMessage.fields.message.as('text')}
			class="message-input"
			autocomplete="off"
			disabled={websocketService.status !== 'CONNECTED'}
			placeholder="Type a message..."
			bind:this={inputField}
		/>
		<input {...sendMessage.fields.channelId.as('hidden', params.channelId)} />
	</form>
</div>

<style>
	.messages-container {
		display: flex;
		flex-direction: column;
		width: 100%;
	}

	.header {
		padding-left: 10px;
	}

	.chatbox-outer {
		overflow-y: auto;
		flex-grow: 1;
		border-top: 1px solid var(--background-accent);
	}

	.chatbox-inner {
		min-height: 100%;
		display: flex;
		flex-direction: column;
	}

	.message {
		margin: 0px 5px 5px;
		border: 1px solid var(--background-secondary);
		border-radius: 10px;
		padding: 10px;
		background-color: var(--background-primary);
		color: var(--text-normal);
		display: flex;
		flex-direction: column;
		gap: 5px;
	}

	.message:first-child {
		margin-top: auto;
	}

	.message-header {
		display: flex;
		gap: 20px;
	}

	.message-username {
		font-size: medium;
		color: var(--text-normal);
	}

	.message-timestamp {
		font-size: small;
		color: var(--text-muted);
	}

	.message-input-container {
		display: flex;
	}

	.message-input {
		flex-grow: 1;
		padding: 15px;
		border-radius: 10px;
		margin: 10px;
		font-size: medium;
	}
	.message-input:focus-visible {
		outline: none;
	}
</style>
