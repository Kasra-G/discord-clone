export interface Message {
	readonly id: string;
	readonly createdAt: string;
	readonly updatedAt: string;
	readonly channelId: string;
	readonly content: string;
	readonly author: User;
}

export interface User {
	readonly id: string;
	readonly createdAt: string;
	readonly updatedAt: string;
	readonly email: string;
	readonly username: string;
}

export interface ClientCommand {
	readonly command: String;
	readonly timestamp: String;
	readonly payload: object;
}
