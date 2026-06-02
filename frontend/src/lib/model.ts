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
	readonly command: string;
	readonly timestamp: string;
	readonly payload: object;
}

export interface LoginFormResponse {
	readonly user?: User;
	readonly error?: string;
	readonly ok: boolean;
}
