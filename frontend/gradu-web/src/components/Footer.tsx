// src/components/Footer.tsx
import { useNavigate } from "react-router-dom";
import { axiosInstance, clearAuth } from "../lib/axios"; // clearAuth는 accessToken/프로필 제거용 유틸이라고 가정
import s from "./Footer.module.css";

export default function Footer() {
  const nav = useNavigate();

  const GMAIL_URL = `https://mail.google.com/mail/?view=cm&fs=1&to=gradu.ate0420@gmail.com&su=${encodeURIComponent(
    "GRADU 문의"
  )}&body=${encodeURIComponent("안녕하세요, GRADU 관련 문의드립니다.\n\n")}`;

  const onWithdraw = async () => {
    const yes = window.confirm(
      "정말 탈퇴하시겠습니까?\n탈퇴 시 저장된 졸업 설계 및 이수 정보가 모두 삭제되며 복구할 수 없습니다."
    );
    if (!yes) return;

    try {
      await axiosInstance.delete("/api/v1/auth/withdraw", {
        withCredentials: true, // refresh 쿠키 같이 전송
      });

      alert("회원탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.");

      // 로컬에 저장된 토큰/프로필 제거
      try {
        clearAuth?.(); // 없으면 아래 개별 삭제
      } catch {
        // 무시
      }

      nav("/login", { replace: true });
    } catch (e: any) {
      alert(
        e?.response?.data?.message ||
          "회원탈퇴 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요."
      );
    }
  };

  return (
    <footer className={s.footer}>
      <div className={s.inner}>
        {/* 왼쪽: 서비스 정보 */}
        <div className={s.brand}>
          <div className={s.logo}>GRADU</div>
          <p className={s.desc}>한동대학교 컴공심화 이수 관리 및 졸업 설계 서비스</p>
          <p className={s.copy}>
            © {new Date().getFullYear()} GRADU. All rights reserved.
          </p>
        </div>

        {/* 오른쪽: 링크 묶음 */}
        <div className={s.links}>
          <div className={s.col}>
            <h4>서비스</h4>
            <a
              href="https://gradu0420.notion.site/Notion-2bdd4780dde180709bd6c3e868fa1360?source=copy_link"
              target="_blank"
              rel="noopener noreferrer"
            >
              소개
            </a>
            <a
              href="https://www.notion.so/gradu0420/2bdd4780dde1809fb930c96e3e7e6fc1?t=new"
              target="_blank"
              rel="noopener noreferrer"
            >
              공지사항
            </a>
          </div>

          <div className={s.col}>
            <h4>문의</h4>
            <span>gradu.ate0420@gmail.com</span>
            <a href={GMAIL_URL} target="_blank" rel="noreferrer">
              문의하기
            </a>
            <button
              type="button"
              onClick={onWithdraw}
              className={s.withdrawBtn}
            >
              회원탈퇴하기
            </button>
          </div>
        </div>
      </div>
    </footer>
  );
}
