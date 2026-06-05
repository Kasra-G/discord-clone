import type { Message } from './model';
import { websocketService } from './websocket.svelte';

export type MessageHandler = (message: Message) => void;

class MessageService {
	private readonly subscribers: Set<MessageHandler> = new Set();
	constructor() {
		websocketService.onMessage((msg) => {
			if (msg.command === 'NEW_MESSAGE')
				this.subscribers.forEach((handle) => handle(msg.payload as Message));
		});
	}

	unsubscribeOnMessage(handler: MessageHandler) {
		this.subscribers.delete(handler);
	}

	subscribeOnMessage(handler: MessageHandler) {
		this.subscribers.add(handler);
		return () => this.unsubscribeOnMessage(handler);
	}
}

export const messageService = new MessageService();
