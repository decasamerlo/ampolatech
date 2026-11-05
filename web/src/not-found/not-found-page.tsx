import { Link } from "@tanstack/react-router";

export function NotFoundPage() {
	return (
		<div className="space-y-2">
			<h1 className="text-2xl font-semibold">Página não encontrada</h1>
			<Link to="/orders" className="text-blue-700 underline">
				Ir para os pedidos
			</Link>
		</div>
	);
}
