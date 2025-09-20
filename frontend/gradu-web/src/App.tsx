import { Link } from "react-router-dom";

export default function App() {
  return (
    <div className="p-6 space-x-4">
      <Link className="underline" to="/login">로그인</Link>
      <Link className="underline" to="/curriculum">커리큘럼 조회</Link>
    </div>
  );
}
