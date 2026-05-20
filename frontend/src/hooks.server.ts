export function handleError({ error }) {
	console.error(error);

	return {
		message: 'Something went wrong on our end.'
	};
}
