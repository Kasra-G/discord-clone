export function handleError({ error, event }) {
	console.error(error);

	return {
		message: 'Something went wrong on our end.'
	};
}
