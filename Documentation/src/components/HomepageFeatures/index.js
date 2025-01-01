import clsx from 'clsx';
import Heading from '@theme/Heading';
import styles from './styles.module.css';


export default function HomepageFeatures() {
  return (
    <section className={styles.features}>
      <div className="container">
        <div className="row">
          <div className="col col--4">
            <div className="text--center">
              <img src="img/arena.png" alt="Feature 1" className={styles.featureImage} />
            </div>
            <div className="text--center padding-horiz--md">
              <h3>Arenas</h3>
              <p>Create, modify, and delete arenas using commands or via the config.</p>
            </div>
          </div>
          <div className="col col--4">
            <div className="text--center">
              <img src="img/kit.png" alt="Feature 2" className={styles.featureImage} />
            </div>
            <div className="text--center padding-horiz--md">
              <h3>Kits</h3>
              <p>Create, modify, and delete kits via GUI or in the config.</p>
            </div>
          </div>
          <div className="col col--4">
            <div className="text--center">
              <img src="img/modifier.png" alt="Feature 3" className={styles.featureImage} />
            </div>
            <div className="text--center padding-horiz--md">
              <h3>Modifiers</h3>
              <p>All items can have custom modifiers. For developers, this system is super dynamic.</p>
            </div>
          </div>
        </div>
      </div>
    </section>  );
}
