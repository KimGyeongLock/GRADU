import s from "./Footer.module.css";

export default function Footer() {
  const GMAIL_URL = `https://mail.google.com/mail/?view=cm&fs=1&to=gradu.ate0420@gmail.com&su=${encodeURIComponent("GRADU 문의")}&body=${encodeURIComponent("안녕하세요, GRADU 관련 문의드립니다.\n\n")}`;
  
  return (
    <footer className={s.footer}>
      <div className={s.inner}>
        {/* 왼쪽: 서비스 정보 */}
        <div className={s.brand}>
          <div className={s.logo}>GRADU</div>
          <p className={s.desc}>
            한동대학교 컴공심화 이수 관리 및 졸업 설계 서비스
          </p>
          <p className={s.copy}>
            © {new Date().getFullYear()} GRADU. All rights reserved.
          </p>
        </div>

        {/* 오른쪽: 링크 묶음 */}
        <div className={s.links}>
          <div className={s.col}>
            <h4>서비스</h4>
            <a href="https://gradu0420.notion.site/Notion-2bdd4780dde180709bd6c3e868fa1360?source=copy_link" target="_blank" rel="noopener noreferrer">소개</a>
            <a href="https://www.notion.so/gradu0420/2bdd4780dde1809fb930c96e3e7e6fc1?t=new" target="_blank" rel="noopener noreferrer">공지사항</a>
            {/* <a href="/faq">자주 묻는 질문</a> */}
          </div>
          {/* 
          <div className={s.col}>
            <h4>약관</h4>
            <a href="/terms">이용약관</a>
            <a href="/privacy">개인정보처리방침</a>
          </div> */}

          <div className={s.col}>
            <h4>문의</h4>
            <span>gradu.ate0420@gmail.com</span>
            <a href={GMAIL_URL} target="_blank" rel="noreferrer">
            문의하기
          </a>

          </div>
        </div>
      </div>
    </footer>
  );
}
