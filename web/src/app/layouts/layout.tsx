import { Outlet } from "@tanstack/react-router";
import { WhoIsSignedIn } from "./who-is-signed-in";

export function Layout() {
	return (
		<div className="min-h-screen bg-slate-50 text-slate-900">
			<header className="flex flex-wrap items-center justify-between gap-4 border-b border-slate-200 bg-white px-6 py-4">
				<span className="text-lg font-semibold">AmpolaTech</span>
				<WhoIsSignedIn />
			</header>
			<main className="p-6">
				<Outlet />
			</main>
		</div>
	);
}
