import s from "./Footer.module.css";

export default function Footer() {
  return (
    <footer className={s.footer}>
      <div className={s.inner}>
        {/* 왼쪽: 서비스 정보 */}
        <div className={s.brand}>
          <div className={s.logo}>GRADU</div>
          <p className={s.desc}>
            대학 이수 관리 및 졸업 설계 서비스
          </p>
          <p className={s.copy}>
            © {new Date().getFullYear()} GRADU. All rights reserved.
          </p>
        </div>

        {/* 오른쪽: 링크 묶음 */}
        <div className={s.links}>
          <div className={s.col}>
            <h4>서비스</h4>
            <a href="/about">소개</a>
            <a href="/notice">공지사항</a>
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
            <a href="/contact">문의하기</a>
          </div>
        </div>
      </div>
    </footer>
  );
}
