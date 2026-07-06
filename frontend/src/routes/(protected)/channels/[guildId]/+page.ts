import { getGuildChannels } from '$lib/backend.remote';
import { redirect } from '@sveltejs/kit';
import type { PageLoad } from './$types';
import { resolve } from '$app/paths';

export const load: PageLoad = async ({ params }) => {
	const channels = await getGuildChannels({ guildId: params.guildId });
	redirect(308, resolve(`/channels/${params.guildId}/${channels[0].id}`));
};
