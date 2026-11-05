// Every request the generated client sends goes through `callApi`, which
// makes the hooks' types true (ADR 0006): their data is the body the spec
// declares for the success, and their error is `ErrorType`.

const JSON_TYPE = "application/json";
const PROBLEM_TYPE = "application/problem+json";

/** The API answered with an error: a status outside 2xx, with a problem details body (RFC 9457). */
export class ApiResponseError<TProblem> extends Error {
	readonly status: number;
	readonly problem: TProblem;

	constructor(status: number, problem: TProblem) {
		super(`The API answered ${status}`);
		this.name = "ApiResponseError";
		this.status = status;
		this.problem = problem;
	}
}

/**
 * The API wasn't reached: the request got no response, or something other
 * than the API answered, such as a proxy with no backend behind it, or the
 * answer is one the spec doesn't declare.
 */
export class ApiUnreachableError extends Error {
	constructor(message: string, options?: ErrorOptions) {
		super(message, options);
		this.name = "ApiUnreachableError";
	}
}

/** The error type of every generated hook. */
export type ErrorType<TProblem> =
	| ApiResponseError<TProblem>
	| ApiUnreachableError;

export const callApi = async <T>(
	url: string,
	init: RequestInit,
): Promise<T> => {
	const headers = new Headers(init.headers);
	headers.set("Accept", `${JSON_TYPE}, ${PROBLEM_TYPE}`);

	let response: Response;
	try {
		response = await fetch(url, { ...init, headers });
	} catch (cause) {
		throw failure(init, cause, `${url} got no response`);
	}

	const type = response.headers
		.get("Content-Type")
		?.split(";")[0]
		?.trim()
		.toLowerCase();
	if (response.ok && type === JSON_TYPE) {
		// The spec declares the body's type, and the backend's tests hold its
		// answers to the spec.
		return (await readJson(url, response, init)) as T;
	}
	if (!response.ok && type === PROBLEM_TYPE) {
		throw new ApiResponseError(
			response.status,
			await readJson(url, response, init),
		);
	}
	throw new ApiUnreachableError(
		`${url} answered ${response.status} as ${type ?? "no content type"}, an answer the spec doesn't declare`,
	);
};

async function readJson(
	url: string,
	response: Response,
	init: RequestInit,
): Promise<unknown> {
	try {
		return await response.json();
	} catch (cause) {
		throw failure(
			init,
			cause,
			`${url} answered ${response.status} with a body that isn't JSON`,
		);
	}
}

// What a request rejects with when a step fails. TanStack Query aborts a
// request it no longer needs, and expects it to reject with the abort's own
// error.
function failure(init: RequestInit, cause: unknown, message: string): unknown {
	return init.signal?.aborted
		? cause
		: new ApiUnreachableError(message, { cause });
}
