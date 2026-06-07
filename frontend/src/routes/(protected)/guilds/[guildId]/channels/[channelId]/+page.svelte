<script lang="ts">
	import { beforeNavigate } from '$app/navigation';
	import { getChannel, getMessages, sendMessage } from '$lib/backend.remote';
	import { messageService } from '$lib/message-service.svelte';
	import { deep } from '$lib/util.svelte.js';
	import { websocketService } from '$lib/websocket.svelte';
	import type { RemoteFormField, RemoteFormFieldValue } from '@sveltejs/kit';
	import { onMount } from 'svelte';
	import * as schemas from '$lib/schemas';

	let { params } = $props();

	let inputField: HTMLElement;
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

	let scroller = $state<DOMRect>();
	let autoscroll = $state(true);
	const autoscrollToLast = (elem: HTMLElement) => {
		scroller?.width;
		scroller?.height;
		if (messages.length <= 0) return;
		if (!autoscroll) return;
		elem.lastElementChild?.scrollIntoView();
	};

	const getChannelDetails = () => {
		return getChannel({ channelId: params.channelId });
	};
</script>

{#snippet showErrors(field: RemoteFormField<RemoteFormFieldValue>)}
	{#each field.issues() as issue, i (i)}
		<div class="issue-text">{issue.message}</div>
	{/each}
{/snippet}

<div class="messages-container">
	<div class="header">
		<h4>#{(await getChannelDetails()).name}</h4>
		<h5>{(await getChannelDetails()).description}</h5>
	</div>
	<div
		bind:contentRect={scroller}
		class="chatbox-outer"
		onwheel={({ currentTarget }) => {
			autoscroll =
				currentTarget.scrollHeight - currentTarget.clientHeight - currentTarget.scrollTop < 50;
		}}
	>
		<div class="chatbox-inner" {@attach autoscrollToLast}>
			{#each messages as msg (msg.id)}
				<div class="message">
					<div class="header">
						<div class="username">
							{msg.author.username}
						</div>
						<div class="timestamp">
							{new Date(msg.createdAt).toLocaleString([], {
								dateStyle: 'short',
								timeStyle: 'short'
							})}
						</div>
					</div>
					<div class="content">
						{msg.content}
					</div>
				</div>
			{/each}
		</div>
	</div>
	<form
		{...sendMessage.preflight(schemas.SEND_MESSAGE)}
		{...sendMessage.enhance(async (form) => {
			if (await form.submit().updates()) {
				form.element.reset();
				inputField.focus();
			}
		})}
		oninput={() => sendMessage.validate()}
		class="message-input-container"
	>
		{@render showErrors(sendMessage.fields.message)}
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
		justify-content: flex-end;
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

		.header {
			display: flex;
			gap: 20px;
		}
		.username {
			font-size: medium;
			color: var(--text-normal);
		}
		.timestamp {
			font-size: small;
			color: var(--text-muted);
		}
		.content {
			text-wrap-mode: wrap;
			overflow-wrap: break-word;
			white-space: break-spaces;
			text-wrap-style: pretty;
			vertical-align: baseline;
		}
	}

	.message-input-container {
		display: flex;
		flex-direction: column;

		.message-input {
			flex-grow: 1;
			max-width: 100%;
			padding: 15px;
			border-radius: 10px;
			margin: 10px;
			font-size: medium;
		}
		.issue-text {
			font-size: small;
			color: var(--red);
		}

		:focus-visible {
			outline: none;
		}
	}
</style>
