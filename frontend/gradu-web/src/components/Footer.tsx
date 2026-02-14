// src/components/Footer.tsx
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { axiosInstance, clearAuth } from "../lib/axios";
import { isGuestMode } from "../lib/auth";
import s from "./Footer.module.css";

const NOTICE_SEEN_KEY = "gradu_notice_seen_v4"; // 새 공지 나오면 v2, v3로 바꾸면 다시 NEW 표시됨

export default function Footer() {
  const nav = useNavigate();
  const [showNoticeNew, setShowNoticeNew] = useState(false);

  useEffect(() => {
    try {
      const seen = localStorage.getItem(NOTICE_SEEN_KEY);
      if (!seen) {
        setShowNoticeNew(true);
      }
    } catch {
      // localStorage 접근 불가한 환경이면 그냥 표시
      setShowNoticeNew(true);
    }
  }, []);

  const GMAIL_URL = `https://mail.google.com/mail/?view=cm&fs=1&to=gradu.ate0420@gmail.com&su=${encodeURIComponent(
    "GRADU 문의"
  )}&body=${encodeURIComponent("안녕하세요, GRADU 관련 문의드립니다.\n\n")}`;

  const onWithdraw = async () => {
    // ✅ 게스트 막기
    if (isGuestMode()) {
      alert(
        "게스트 모드에서는 회원탈퇴 기능을 이용할 수 없습니다.\n로그인 후 다시 시도해 주세요."
      );
      return;
    }

    const yes = window.confirm(
      "정말 탈퇴하시겠습니까?\n탈퇴 시 저장된 졸업 설계 및 이수 정보가 모두 삭제되며 복구할 수 없습니다."
    );
    if (!yes) return;

    try {
      await axiosInstance.delete("/api/v1/auth/withdraw", {
        withCredentials: true, // refresh 쿠키 같이 전송
      });

      alert("회원탈퇴가 완료되었습니다. 그동안 이용해 주셔서 감사합니다.");

      try {
        clearAuth?.();
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

  const handleNoticeClick = () => {
    try {
      localStorage.setItem(NOTICE_SEEN_KEY, "1");
    } catch {
      // 무시
    }
    setShowNoticeNew(false);
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
              onClick={handleNoticeClick}
              className={s.noticeLink}
            >
              <span>공지사항</span>
              {showNoticeNew && (
                <span className={s.noticeNewDot} aria-label="새로운 공지" />
              )}
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
